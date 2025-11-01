# Technical Documentation: PDF Utilities App

## Technology Stack

### Primary Technologies

- **Java**: Main programming language for the application
- **JavaFX**: GUI framework for creating the desktop user interface
- **Apache PDFBox**: Core library for PDF manipulation and processing

### Additional Libraries

- **Apache POI**: For DOCX conversion functionality
- **ControlsFX**: Enhanced UI controls for JavaFX
- **PDFRenderer**: For PDF to image conversion (if needed as alternative)

## Development Environment

- **JDK 11 or higher**: For Java development
- **Maven**: Build automation and dependency management
- **IDE**: IntelliJ IDEA or Eclipse with JavaFX support

## Project Structure

```
PDFUtilities-Java/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/pdfutilities/app/
│   │   │   │   ├── controller/
│   │   │   │   ├── model/
│   │   │   │   ├── view/
│   │   │   │   ├── service/
│   │   │   │   └── Main.java
│   │   │   └── resources/
│   │   │       ├── fxml/
│   │   │       ├── css/
│   │   │       └── images/
│   └── test/
│       └── java/
├── pom.xml
└── README.md
```

## Build System

- **Maven** for dependency management and build automation
- Dependencies will be managed through pom.xml

## Key Design Patterns

- **MVC (Model-View-Controller)**: For separating concerns in the GUI application
- **Service Layer**: For business logic and PDF processing operations
- **Factory Pattern**: For creating different PDF processing strategies

## Performance Considerations

- Asynchronous processing for long-running operations
- Progress indicators for user feedback
- Memory management for large PDF files

## Security

- Local file processing only (no cloud upload)
- Input validation for file paths and operations
- Secure handling of temporary files
- Password management with local storage only
- Encrypted password field handling for PDF files

## Password Management System

### Components

- **SavedPassword Model**: Represents a saved password entry with name, value, and timestamps
- **PasswordManager Service**: Handles CRUD operations for saved passwords with persistent storage
- **PasswordManagementDialogController**: Manages the password management dialog UI
- **MainController Integration**: Integrates password management with the main application

### Features

- **Visual Highlighting**: Password fields are highlighted for encrypted PDFs using CSS styling
- **Persistent Storage**: Passwords are saved to local filesystem using Java serialization
- **User Interface**: Integrated toolbar controls and management dialog
- **Security**: All password data remains local to the user's machine
- **Enhanced Dropdown**: Password fields now include a dropdown button (▼) for quick access to saved passwords
- **Context Menu**: Right-click dropdown shows all saved passwords with "Manage Passwords" option
- **Seamless Integration**: Password selection from dropdown automatically applies to the selected file

### File Structure

```
src/main/java/com/pdfutilities/app/
├── model/
│   └── SavedPassword.java
├── service/
│   └── PasswordManager.java
├── controller/
│   ├── PasswordManagementDialogController.java
│   └── MainController.java (enhanced)
└── resources/
    ├── css/
    │   └── password-highlight.css
    └── fxml/
        └── password_management_dialog.fxml
```
