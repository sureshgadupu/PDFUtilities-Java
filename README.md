# PDF Utilities App

A comprehensive Java-based desktop application for PDF manipulation and management. This application provides a user-friendly interface for performing common PDF operations including conversion, compression, merging, splitting, text extraction, and image conversion.

## 🚀 Features

### 📄 **Convert PDF to DOCX**

- Convert PDF documents to editable Microsoft Word format
- Preserve formatting and layout during conversion
- Batch processing support for multiple files

### 🗜️ **Compress PDF**

- Reduce PDF file size with configurable compression levels
- **Compression Options:**
  - Smallest (Low Quality) - Maximum compression
  - Balanced (Medium Quality) - Optimal balance
  - Largest (High Quality) - Minimal quality loss
- Target file size specification support

### 🔗 **Merge PDFs**

- Combine multiple PDF files into a single document
- Drag-and-drop file reordering
- Maintain original quality and bookmarks

### ✂️ **Split PDF**

- **Multiple Split Modes:**
  - Every Page - Split each page into separate files
  - Custom Range - Specify exact page ranges (e.g., 1-3, 5, 8-10)
  - Size-Based - Split by target file size

### 📝 **Extract Content**

- **Text Extraction:** Extract plain text from PDFs to .txt files
- **Image Extraction:** Extract all embedded images as separate files
- OCR support for scanned documents

### 🖼️ **Convert to Images**

- **Output Formats:** JPG and PNG support
- **Conversion Modes:**
  - Entire PDF to single image
  - Each page to separate image
- **Quality Settings:** Configurable DPI (96, 150, 300)
- **Color Options:** Color or Black & White output

## 🛠️ Technology Stack

- **Java 24** - Core programming language
- **JavaFX 24** - Modern desktop GUI framework
- **Apache PDFBox 2.0.30** - PDF manipulation library
- **Apache POI 5.2.5** - DOCX conversion support
- **ControlsFX 11.1.2** - Enhanced UI controls
- **Maven** - Build automation and dependency management

## 📋 Prerequisites

- **Java 24** or higher
- **Maven 3.6+** (for building from source)
- **Windows 10/11**, **macOS 10.15+**, or **Linux** (Ubuntu 22.04+)

## 🚀 Quick Start

### Option 1: Download Pre-built JAR

1. Download the latest release from [Releases](../../releases)
2. Run the application:
   ```bash
   java -jar pdf-utilities-app-1.0.1.jar
   ```

### Option 2: Build from Source

1. **Clone the repository:**

   ```bash
   git clone https://github.com/sureshgadupu/PDFUtilities-Java
   cd PDFUtilities-Java
   ```

2. **Build the application:**

   ```bash
   mvn clean package
   ```

3. **Run the application:**

   ```bash
   # Using Maven
   mvn javafx:run

   # Or using the JAR
   java -jar target/pdf-utilities-app-1.0.1.jar

   # On Windows, use the provided batch file
   run.bat
   ```

````

## 🎯 Usage Guide

### Basic Workflow

1. **Launch the application** - The main window will open with all features accessible
2. **Select files** - Click "Select Files" or drag-and-drop PDF files into the application
3. **Choose operation** - Click on the desired operation icon (Convert, Compress, Merge, etc.)
4. **Configure options** - Set operation-specific parameters in the respective tab
5. **Set output location** - Choose "Same as Input" or specify a custom output folder
6. **Process files** - Click the action button to start processing

### File Selection

- **Single/Multiple Files:** Use Ctrl+Click or Shift+Click for multiple selection
- **Folder Selection:** Process all PDFs in a directory
- **Drag & Drop:** Simply drag files from your file manager

### Output Management

- **Same as Input:** Saves processed files alongside originals
- **Custom Folder:** Designate a specific output directory
- **File Naming:** Automatic naming with operation suffixes

## 🏗️ Project Structure

```
PDFUtilities-Java/
├── src/main/java/com/pdfutilities/app/
│   ├── Main.java                 # Application entry point
│   ├── controller/
│   │   └── MainController.java   # Main UI controller
│   ├── model/
│   │   └── FileItem.java         # File representation model
│   └── service/                  # Business logic services
│       ├── BasePDFService.java   # Base service class
│       ├── PDFService.java       # Main service coordinator
│       ├── PDFCompressionService.java
│       ├── PDFMergeService.java
│       ├── PDFSplitService.java
│       ├── PDFToImageService.java
│       ├── TextExtractionService.java
│       └── DocxConversionService.java
├── src/main/resources/
│   ├── fxml/main.fxml           # Main UI layout
│   ├── css/                     # Stylesheets
│   └── images/                  # Application icons
├── docs/                        # Documentation
├── target/                      # Build artifacts
└── pom.xml                      # Maven configuration
```

## 🔧 Development

### Setting up Development Environment

1. **Install Java 21:**

   ```bash
   # Windows (using Chocolatey)
   choco install openjdk21

   # macOS (using Homebrew)
   brew install openjdk@21

   # Ubuntu/Debian
   sudo apt install openjdk-21-jdk
   ```

2. **Install Maven:**

   ```bash
   # Windows (using Chocolatey)
   choco install maven

   # macOS (using Homebrew)
   brew install maven

   # Ubuntu/Debian
   sudo apt install maven
   ```

3. **IDE Setup:**
   - **IntelliJ IDEA:** Import as Maven project
   - **Eclipse:** Import as existing Maven project
   - **VS Code:** Install Java Extension Pack

### Running Tests

```bash
mvn test
```

### Building Distribution

```bash
# Create executable JAR
mvn clean package

# The JAR will be available at: target/pdf-utilities-app-1.0.1.jar
```

## 📊 Performance Characteristics

- **Memory Usage:** Optimized for handling large PDF files (100MB+)
- **Processing Speed:** Multi-threaded operations for batch processing
- **Progress Tracking:** Real-time progress bars for all operations
- **Error Handling:** Graceful handling of corrupted or password-protected PDFs

## 🔒 Security & Privacy

- **Local Processing:** All operations performed locally on your machine
- **No Data Upload:** Files never leave your computer
- **Temporary Files:** Secure cleanup of temporary processing files
- **Input Validation:** Comprehensive validation of file paths and formats

## 🐛 Troubleshooting

### Common Issues

**"JavaFX runtime components are missing"**

- Ensure Java 21+ is installed
- Try running with: `mvn javafx:run`

**"Out of memory" errors with large PDFs**

- Increase heap size: `java -Xmx2g -jar pdf-utilities-app-1.0.1.jar`

**"Permission denied" on macOS/Linux**

- Run with: `chmod +x run.sh` (if using shell script)

### Debug Mode

Enable detailed logging:

```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar pdf-utilities-app-1.0.1.jar
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes
4. Add tests for new functionality
5. Run tests: `mvn test`
6. Commit changes: `git commit -am 'Add feature'`
7. Push to branch: `git push origin feature-name`
8. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Apache PDFBox** team for excellent PDF processing capabilities
- **OpenJFX** community for modern JavaFX framework
- **ControlsFX** team for enhanced UI components

## 📞 Support

- **Issues:** [GitHub Issues](../../issues)
- **Discussions:** [GitHub Discussions](../../discussions)
- **Documentation:** [Wiki](../../wiki)

---

**Made with ❤️ by the PDF Utilities Team**
