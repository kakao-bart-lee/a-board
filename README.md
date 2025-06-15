# a-board

A simple anonymous board built with Spring Boot WebFlux and Kotlin.
It supports OAuth2 login (Google and Facebook), posting text with optional images and gender display,
comments with one level of replies, and user management.
Authentication is handled via JWT tokens issued by `/auth/token`. Each token contains a random anonymous ID so the real user ID is hidden when posting.
In the `dev` profile data is kept in memory while the `prod` profile
stores data in PostgreSQL via Spring Data R2DBC. The default profile is
`dev` and you can switch to production by setting `SPRING_PROFILES_ACTIVE=prod`.
Posts track how many times they were viewed and both posts and comments can be soft deleted.
Users carry additional profile information such as gender, birth year, multiple profile image URLs, location, preferred language, a short introduction and a role (`USER`, `MODERATOR` or `ADMIN`). Users may also be temporarily suspended. Posts can be reported and later moderated by a `MODERATOR` or `ADMIN`.

## Endpoints
- `POST /users` create user (name, gender, birthYear, profileImageUrls, location,
  preferredLanguage, aboutMe)
- `GET /users` list users
- `GET /users/{id}` get user by id
- `DELETE /users/{id}` delete user (posts remain)
- `POST /users/{id}/suspend` suspend user for a number of minutes
- `POST /posts` create post (anonymous ID from JWT)
- `PUT /posts/{id}` update post (author or admin)
- `POST /posts/{id}/comments` add comment (anonymous ID from JWT and optional `parentCommentId`)
- `POST /posts/{id}/report` report a post
- `GET /posts/reported` list reported posts (admin or moderator)
- `POST /posts/{id}/moderate?delete=true` clear reports or delete a post (admin or moderator)
- `POST /auth/token` obtain JWT for a user
- `POST /users` create a user without authentication
- `DELETE /posts/{id}` soft delete a post
- `DELETE /posts/{postId}/comments/{commentId}` soft delete a comment

API documentation is available at `/swagger-ui.html` when the server is running.

## CLI Example

The `cli/register_login_cli.py` script provides an interactive way to try the
API. It lets you register or log in and then list posts, read them, create new
ones and add comments. After logging in, the script prints your JWT so you can
reuse it in other tools if needed.

Start the tool (with the server running on `localhost:8080`):

```bash
python3 cli/register_login_cli.py
```

You will be prompted to register or log in, after which commands such as
`list`, `read <id>`, `new` and `comment <id>` become available.
