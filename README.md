# a-board

A simple anonymous board built with Spring Boot WebFlux and Kotlin.
It supports OAuth2 login (Google, Apple and Facebook), posting text with optional images and gender display,
comments with one level of replies, and user management.
Authenticated users receive a temporary anonymous ID stored in the session so their real ID is never exposed.
Posts track how many times they were viewed and both posts and comments can be soft deleted.
Users carry additional profile information such as gender, birth year, multiple
profile image URLs, location, preferred language and a short introduction.

## Endpoints
- `POST /users` create user (name, gender, birthYear, profileImageUrls, location,
  preferredLanguage, aboutMe)
- `GET /users` list users
- `GET /users/{id}` get user by id
- `POST /posts` create post (session anonymous ID is used)
- `POST /posts/{id}/comments` add comment (session anonymous ID is used and optional `parentCommentId`)
- `DELETE /posts/{id}` soft delete a post
- `DELETE /posts/{postId}/comments/{commentId}` soft delete a comment

API documentation is available at `/swagger-ui.html` when the server is running.
