# Shortify - Backend

Shortify is a URL shortener built with Spring Boot. The project provides an API for shortening long URLs and retrieving the original URLs from short ones. It implements JWT-based authorization for secure access to the service.

## Features

- **URL shortening:** Convert long URLs into short, shareable links.
- **JWT-based Authentication:** Secure API access using JSON Web Tokens (JWT).
- **User management:** Register and log in users with JWT tokens for authentication.
- **Retrieve original URL:** Given a shortened URL, retrieve the original URL.
- **Analytics:** Track and view URL statistics such as total clicks, user-specific analytics, and overall usage.
- **My URLs:** View a list of URLs that the authenticated user has shortened.

## Tech Stack

- **Backend:** Spring Boot
- **Authentication:** JWT (JSON Web Tokens)
- **Database:** PostgreSQL
- **Java version:** 21 
- **Libraries/Frameworks:**
    - Spring Security
    - Spring Data JPA
    - Spring Web
    - JJWT (for handling JWT)

## Requirements

- Java 21 or above
- Maven or Gradle for dependency management
- Database (H2 for development or any other relational database)

## Getting Started

### Clone the repository:

```bash
git clone https://github.com/GouravBakshi/Shortify-Backend.git
cd Shortify-Backend

```

### Check out the live link here :
 - https://shfy.netlify.app

### Frontend Link :
- https://github.com/GouravBakshi/Shortify-Frontend


### Enjoy Using it.

