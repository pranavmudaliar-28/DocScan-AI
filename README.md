# DocScan AI

DocScan AI is a full-stack document scanning and management solution that leverages Artificial Intelligence for enhanced document processing. The system consists of three main components: a mobile application for scanning, a web application for management, and a robust backend API.

## Project Structure

This repository is organized as a monorepo containing the following components:

- **[`docscan-android`](./docscan-android/)**: The Android client application built with Kotlin and Jetpack Compose. It provides mobile document scanning capabilities.
- **[`docscan-api`](./docscan-api/)**: The backend REST API built with NestJS. It handles user authentication, document processing, AI integrations, and cloud storage management.
- **[`docscan-web`](./docscan-web/)**: The web client application built with Next.js, React, and Tailwind CSS (shadcn-ui). It serves as the main dashboard for users to upload, manage, and view their documents from any web browser.

## Getting Started

### Prerequisites

- Node.js (v18+)
- Java JDK 17 (for Android)
- Android Studio
- Docker (optional, for running services locally)

### Web App (docscan-web)

1. Navigate to the web directory:
   ```bash
   cd docscan-web
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```

### Backend API (docscan-api)

1. Navigate to the api directory:
   ```bash
   cd docscan-api
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run start:dev
   ```

### Android App (docscan-android)

1. Open the `docscan-android` directory in Android Studio.
2. Sync the project with Gradle files.
3. Build and run the app on an emulator or physical device.

## License

This project is licensed under the MIT License.
