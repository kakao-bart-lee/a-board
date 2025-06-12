# a-board

A simple anonymous board built with Spring Boot WebFlux and Kotlin.
It supports OAuth2 login (Google, Apple and Facebook), posting text with optional images and gender display,
comments with one level of replies, and user management.
Authentication is handled via JWT tokens issued by `/auth/token`. Each token contains a random anonymous ID so the real user ID is hidden when posting.
Data is persisted in PostgreSQL via Spring Data R2DBC.
Posts track how many times they were viewed and both posts and comments can be soft deleted.
Users carry additional profile information such as gender, birth year, multiple profile image URLs, location, preferred language, a short introduction and a role (`USER` or `ADMIN`).

## Endpoints
- `POST /users` create user (name, gender, birthYear, profileImageUrls, location,
  preferredLanguage, aboutMe)
- `GET /users` list users
- `GET /users/{id}` get user by id
- `POST /posts` create post (anonymous ID from JWT)
- `POST /posts/{id}/comments` add comment (anonymous ID from JWT and optional `parentCommentId`)
- `POST /auth/token` obtain JWT for a user
- `DELETE /posts/{id}` soft delete a post
- `DELETE /posts/{postId}/comments/{commentId}` soft delete a comment

API documentation is available at `/swagger-ui.html` when the server is running.
