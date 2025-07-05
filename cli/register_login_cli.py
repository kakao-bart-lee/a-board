"""Simple interactive CLI for testing the board API.

This script allows you to register, verify your email, log in, and then
interact with the board. Once authenticated you can list posts, read a post,
create a new post and add comments.
"""

import argparse
import json
from typing import Optional

import requests


class BoardCLI:
    def __init__(self, api_base: str):
        self.api_base = api_base.rstrip("/")
        self.email: Optional[str] = None
        self.token: Optional[str] = None

    def _auth_headers(self) -> dict:
        """Return Authorization header using the current token."""
        if not self.token:
            raise ValueError("Authentication required. Please log in first.")
        return {"Authorization": f"Bearer {self.token}"}

    # -------- authentication ---------
    def signup(self, name: str, email: str, password: str, gender: str, birth_year: int) -> dict:
        data = {
            "name": name,
            "email": email,
            "password": password,
            "gender": gender,
            "birthYear": birth_year,
        }
        r = requests.post(f"{self.api_base}/auth/signup", json=data)

        try:
            r.raise_for_status()
        except requests.HTTPError as e:
            print(f"Signup failed: {e}")
            print(r.text)
            return {}

        print("Signup successful! Please check your email (or server logs) for the verification code.")
        return r.json()

    def verify_email(self, email: str, code: str) -> bool:
        data = {"email": email, "code": code}
        r = requests.post(f"{self.api_base}/auth/verify", json=data)

        try:
            r.raise_for_status()
            print("Email verified successfully!")
            return True
        except requests.HTTPError as e:
            print(f"Email verification failed: {e}")
            print(r.text)
            return False

    def login(self, email: str, password: str) -> Optional[str]:
        data = {"email": email, "password": password}
        r = requests.post(f"{self.api_base}/auth/token", json=data)

        try:
            r.raise_for_status()
        except requests.HTTPError as e:
            print(f"Login failed: {e}")
            print(r.text)
            return None

        self.email = email
        return r.json()["token"]

    # -------- post operations ---------
    def list_posts(self) -> list[dict]:
        """Fetch posts using authentication."""
        headers = self._auth_headers()
        r = requests.get(f"{self.api_base}/posts", headers=headers)
        r.raise_for_status()
        return r.json()

    def get_post(self, post_id: str) -> dict:
        """Retrieve a single post using authentication."""
        headers = self._auth_headers()
        r = requests.get(f"{self.api_base}/posts/{post_id}", headers=headers)
        r.raise_for_status()
        return r.json()

    def create_post(self, text: str, image_url: str | None, gender: str | None) -> dict:
        headers = self._auth_headers()
        data = {"text": text, "imageUrl": image_url, "gender": gender}
        r = requests.post(f"{self.api_base}/posts", json=data, headers=headers)
        r.raise_for_status()
        return r.json()

    def add_comment(self, post_id: str, text: str, parent_comment_id: str | None = None) -> dict:
        headers = self._auth_headers()
        data = {"text": text, "parentCommentId": parent_comment_id}
        r = requests.post(
            f"{self.api_base}/posts/{post_id}/comments", json=data, headers=headers
        )
        r.raise_for_status()
        return r.json()

    # -------- menus ---------
    def auth_menu(self) -> bool:
        print("\n--- Welcome to A-Board ---")
        choice = input("(s)ignup, (l)ogin or (q)uit? ").strip().lower()

        if choice == 's':
            print("\n[Signup]")
            name = input("Name: ")
            email = input("Email: ")
            password = input("Password: ")
            gender = input("Gender (MALE/FEMALE/OTHER): ").upper()
            birth_year = int(input("Birth year: "))
            user_data = self.signup(name, email, password, gender, birth_year)
            if not user_data:
                return True # Stay in auth menu

            print("\n[Verify Email]")
            code = input(f"Enter verification code for {email}: ")
            if self.verify_email(email, code):
                print("\n[Login]")
                self.token = self.login(email, password)
                if self.token:
                    print("Login successful!")
            return True

        elif choice == 'l':
            print("\n[Login]")
            email = input("Email: ")
            password = input("Password: ")
            self.token = self.login(email, password)
            if self.token:
                print("Login successful!")
            return True

        elif choice == 'q':
            return False

        else:
            print("Invalid choice.")
            return True

    def main_menu(self) -> bool:
        print(f"\n--- Logged in as {self.email} ---")
        cmd = input(
            "Enter command (list, read <id>, new, comment <id>, logout, quit): "
        ).strip()
        try:
            if cmd == "list":
                posts = self.list_posts()
                if not posts:
                    print("No posts found.")
                for p in posts:
                    print(f"ID: {p['id']} | Views: {p.get('viewCount', 0)} | Text: {p['text'][:50]}...")
            elif cmd.startswith("read"):
                parts = cmd.split(maxsplit=1)
                if len(parts) == 2:
                    post = self.get_post(parts[1])
                    print(json.dumps(post, indent=2, ensure_ascii=False))
                else:
                    print("Usage: read <post_id>")
            elif cmd == "new":
                text = input("Text: ")
                image_url = input("Image URL (optional): ") or None
                gender = input("Gender (optional): ") or None
                post = self.create_post(text, image_url, gender)
                print("Created:")
                print(json.dumps(post, indent=2, ensure_ascii=False))
            elif cmd.startswith("comment"):
                parts = cmd.split(maxsplit=1)
                if len(parts) != 2:
                    print("Usage: comment <post_id>")
                else:
                    text = input("Text: ")
                    parent_id = input("Parent comment ID (optional): ") or None
                    comment = self.add_comment(parts[1], text, parent_id)
                    print("Added comment:")
                    print(json.dumps(comment, indent=2, ensure_ascii=False))
            elif cmd == "logout":
                self.token = None
                self.email = None
                print("Logged out.")
            elif cmd == "quit":
                return False
            else:
                print("Unknown command.")
        except (requests.HTTPError, ValueError) as e:
            print(f"An error occurred: {e}")

        return True

    def run(self) -> None:
        running = True
        while running:
            if not self.token:
                running = self.auth_menu()
            else:
                running = self.main_menu()
        print("Goodbye!")


def main() -> None:
    parser = argparse.ArgumentParser(description="Interactive board CLI")
    parser.add_argument("--api-base", default="http://localhost:8080")
    args = parser.parse_args()

    cli = BoardCLI(args.api_base)
    cli.run()


if __name__ == '__main__':
    main()