"""Simple interactive CLI for testing the board API.

This script allows you to register or log in and then interact with the
board. Once authenticated you can list posts, read a post, create a new
post and add comments.
"""

import argparse
import json
from typing import Optional

import requests


class BoardCLI:
    def __init__(self, api_base: str):
        self.api_base = api_base.rstrip("/")
        self.user_id: Optional[str] = None
        self.token: Optional[str] = None

    # -------- authentication ---------
    def create_user(self, name: str, gender: str, birth_year: int) -> dict:
        data = {
            "name": name,
            "gender": gender,
            "birthYear": birth_year,
            "profileImageUrls": [],
            "location": None,
            "preferredLanguage": None,
            "aboutMe": None,
            "role": "USER",
        }
        r = requests.post(f"{self.api_base}/users", json=data)
        r.raise_for_status()
        return r.json()

    def get_token(self, user_id: str) -> str:
        data = {"userId": user_id}
        r = requests.post(f"{self.api_base}/auth/token", json=data)
        r.raise_for_status()
        return r.json()["token"]

    # -------- post operations ---------
    def list_posts(self) -> list[dict]:
        r = requests.get(f"{self.api_base}/posts")
        r.raise_for_status()
        return r.json()

    def get_post(self, post_id: str) -> dict:
        r = requests.get(f"{self.api_base}/posts/{post_id}")
        r.raise_for_status()
        return r.json()

    def create_post(self, text: str, image_url: str | None, gender: str | None) -> dict:
        headers = {"Authorization": f"Bearer {self.token}"}
        data = {"text": text, "imageUrl": image_url, "gender": gender}
        r = requests.post(f"{self.api_base}/posts", json=data, headers=headers)
        r.raise_for_status()
        return r.json()

    def add_comment(self, post_id: str, text: str, parent_comment_id: str | None = None) -> dict:
        headers = {"Authorization": f"Bearer {self.token}"}
        data = {"text": text, "parentCommentId": parent_comment_id}
        r = requests.post(
            f"{self.api_base}/posts/{post_id}/comments", json=data, headers=headers
        )
        r.raise_for_status()
        return r.json()

    # -------- menus ---------
    def auth_menu(self) -> bool:
        choice = input("(r)egister, (l)ogin or (q)uit? ").strip().lower()
        if choice == "r":
            name = input("Name: ")
            gender = input("Gender: ")
            birth_year = int(input("Birth year: "))
            user = self.create_user(name, gender, birth_year)
            print("Created user:")
            print(json.dumps(user, indent=2))
            self.user_id = user["id"]
            self.token = self.get_token(self.user_id)
            print("\nLogged in with token")
            return True
        elif choice == "l":
            self.user_id = input("User id: ")
            self.token = self.get_token(self.user_id)
            print("\nLogged in.")
            return True
        elif choice == "q":
            return False
        return True

    def main_menu(self) -> bool:
        cmd = input(
            "Enter command (list, read <id>, new, comment <id>, logout, quit): "
        ).strip()
        if cmd == "list":
            posts = self.list_posts()
            for p in posts:
                print(f"{p['id']}: {p['text']} (views {p.get('viewCount', 0)})")
        elif cmd.startswith("read"):
            parts = cmd.split(maxsplit=1)
            if len(parts) == 2:
                post = self.get_post(parts[1])
                print(json.dumps(post, indent=2))
            else:
                print("Usage: read <post_id>")
        elif cmd == "new":
            text = input("Text: ")
            image_url = input("Image URL (optional): ") or None
            gender = input("Gender (optional): ") or None
            post = self.create_post(text, image_url, gender)
            print("Created:")
            print(json.dumps(post, indent=2))
        elif cmd.startswith("comment"):
            parts = cmd.split(maxsplit=1)
            if len(parts) != 2:
                print("Usage: comment <post_id>")
            else:
                text = input("Text: ")
                parent_id = input("Parent comment ID (optional): ") or None
                comment = self.add_comment(parts[1], text, parent_id)
                print("Added comment:")
                print(json.dumps(comment, indent=2))
        elif cmd == "logout":
            self.token = None
            self.user_id = None
        elif cmd == "quit":
            return False
        return True

    def run(self) -> None:
        running = True
        while running:
            if not self.token:
                running = self.auth_menu()
            else:
                running = self.main_menu()


def main() -> None:
    parser = argparse.ArgumentParser(description="Interactive board CLI")
    parser.add_argument("--api-base", default="http://localhost:8080")
    args = parser.parse_args()

    cli = BoardCLI(args.api_base)
    cli.run()


if __name__ == '__main__':
    main()
