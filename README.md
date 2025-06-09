# a-board

A simple anonymous board built with Spring Boot WebFlux and Kotlin.
It supports OAuth2 login, posting text with optional images and gender display,
comments with one level of replies, and basic user management storing author IDs.

## Endpoints
- `POST /users` create user
- `GET /users` list users
- `POST /posts` create post (provide `authorId`)
- `POST /posts/{id}/comments` add comment (provide `authorId` and optional `parentCommentId`)
