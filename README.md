# a-board

A simple anonymous board built with Spring Boot WebFlux and Kotlin.
It supports OAuth2 login, posting text with optional images and gender display,
comments with one level of replies, and basic user management storing author IDs.
Users carry additional profile information such as gender, birth year, multiple
profile image URLs, location, preferred language and a short introduction.

## Endpoints
- `POST /users` create user (name, gender, birthYear, profileImageUrls, location,
  preferredLanguage, aboutMe)
- `GET /users` list users
- `GET /users/{id}` get user by id
- `POST /posts` create post (provide `authorId`)
- `POST /posts/{id}/comments` add comment (provide `authorId` and optional `parentCommentId`)
