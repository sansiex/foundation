// File Upload Management
class FileUploadManager {
    constructor() {
        this.selectedFile = null;
        this.maxFileSize = 10 * 1024 * 1024; // 10MB
        this.allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        this.initializeElements();
        this.bindEvents();
    }

    initializeElements() {
        this.elements = {
            fileInput: document.getElementById('file-input'),
            fileUploadZone: document.getElementById('file-upload-zone'),
            filePreview: document.getElementById('file-preview'),
            removeFileBtn: document.getElementById('remove-file'),
            messageInput: document.getElementById('message-input')
        };
    }

    bindEvents() {
        // File input change
        if (this.elements.fileInput) {
            this.elements.fileInput.addEventListener('change', this.handleFileSelect.bind(this));
        }

        // Drag and drop events
        if (this.elements.messageInput) {
            const inputWrapper = this.elements.messageInput.closest('.input-wrapper');
            if (inputWrapper) {
                this.setupDragAndDrop(inputWrapper);
            }
        }

        // Remove file button
        if (this.elements.removeFileBtn) {
            this.elements.removeFileBtn.addEventListener('click', this.clearSelection.bind(this));
        }

        // Paste event for images
        document.addEventListener('paste', this.handlePaste.bind(this));
    }

    setupDragAndDrop(element) {
        element.addEventListener('dragover', this.handleDragOver.bind(this));
        element.addEventListener('dragenter', this.handleDragEnter.bind(this));
        element.addEventListener('dragleave', this.handleDragLeave.bind(this));
        element.addEventListener('drop', this.handleDrop.bind(this));
    }

    handleDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'copy';
    }

    handleDragEnter(e) {
        e.preventDefault();
        e.target.closest('.input-wrapper')?.classList.add('drag-over');
    }

    handleDragLeave(e) {
        e.preventDefault();
        if (!e.relatedTarget || !e.currentTarget.contains(e.relatedTarget)) {
            e.target.closest('.input-wrapper')?.classList.remove('drag-over');
        }
    }

    handleDrop(e) {
        e.preventDefault();
        e.target.closest('.input-wrapper')?.classList.remove('drag-over');
        
        const files = Array.from(e.dataTransfer.files);
        if (files.length > 0) {
            this.selectFile(files[0]);
        }
    }

    handlePaste(e) {
        const items = Array.from(e.clipboardData?.items || []);
        const imageItem = items.find(item => item.type.startsWith('image/'));
        
        if (imageItem) {
            e.preventDefault();
            const file = imageItem.getAsFile();
            if (file) {
                this.selectFile(file);
            }
        }
    }

    handleFileSelect(e) {
        const file = e.target.files[0];
        if (file) {
            this.selectFile(file);
        }
    }

    selectFile(file) {
        // Validate file
        const validation = this.validateFile(file);
        if (!validation.valid) {
            window.app.ui.showToast(validation.error, 'error');
            return;
        }

        this.selectedFile = file;
        this.showFilePreview(file);
        window.app.ui.showToast(`File selected: ${file.name}`, 'success');
    }

    validateFile(file) {
        // Check file type
        if (!this.allowedTypes.includes(file.type)) {
            return {
                valid: false,
                error: `File type not supported. Allowed types: ${this.allowedTypes.join(', ')}`
            };
        }

        // Check file size
        if (file.size > this.maxFileSize) {
            return {
                valid: false,
                error: `File size too large. Maximum size: ${this.formatFileSize(this.maxFileSize)}`
            };
        }

        return { valid: true };
    }

    showFilePreview(file) {
        if (!this.elements.fileUploadZone || !this.elements.filePreview) {
            return;
        }

        // Show upload zone
        this.elements.fileUploadZone.classList.remove('hidden');

        // Create preview
        const preview = this.createFilePreview(file);
        this.elements.filePreview.innerHTML = preview;

        // Scroll to show preview
        this.elements.fileUploadZone.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    createFilePreview(file) {
        const fileSize = this.formatFileSize(file.size);
        
        // Create image preview if possible
        if (file.type.startsWith('image/')) {
            const imageUrl = URL.createObjectURL(file);
            
            // Clean up URL after a delay
            setTimeout(() => URL.revokeObjectURL(imageUrl), 60000);
            
            return `
                <div class="file-preview-card">
                    <div class="file-thumbnail">
                        <img src="${imageUrl}" alt="${file.name}" onload="this.parentElement.classList.add('loaded')">
                    </div>
                    <div class="file-details">
                        <div class="file-name" title="${file.name}">${this.truncateFilename(file.name)}</div>
                        <div class="file-meta">
                            <span>${fileSize}</span>
                            <span>${file.type}</span>
                        </div>
                    </div>
                </div>
            `;
        } else {
            // Generic file preview
            return `
                <div class="file-preview-card">
                    <div class="file-thumbnail">
                        <span class="file-icon">📄</span>
                    </div>
                    <div class="file-details">
                        <div class="file-name" title="${file.name}">${this.truncateFilename(file.name)}</div>
                        <div class="file-meta">
                            <span>${fileSize}</span>
                            <span>${file.type}</span>
                        </div>
                    </div>
                </div>
            `;
        }
    }

    clearSelection() {
        this.selectedFile = null;
        
        // Hide upload zone
        if (this.elements.fileUploadZone) {
            this.elements.fileUploadZone.classList.add('hidden');
        }
        
        // Clear file input
        if (this.elements.fileInput) {
            this.elements.fileInput.value = '';
        }
        
        // Clear preview
        if (this.elements.filePreview) {
            this.elements.filePreview.innerHTML = '';
        }
    }

    getSelectedFile() {
        return this.selectedFile;
    }

    hasSelectedFile() {
        return this.selectedFile !== null;
    }

    // Utility methods
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    truncateFilename(filename, maxLength = 30) {
        if (filename.length <= maxLength) {
            return filename;
        }
        
        const extension = filename.split('.').pop();
        const nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        const truncatedName = nameWithoutExt.substring(0, maxLength - extension.length - 3);
        
        return `${truncatedName}...${extension}`;
    }

    // Upload progress (for future enhancement)
    showUploadProgress(progress) {
        const progressBar = document.querySelector('.upload-progress-bar');
        if (progressBar) {
            progressBar.style.width = `${progress}%`;
        }
    }

    hideUploadProgress() {
        const progressContainer = document.querySelector('.upload-progress');
        if (progressContainer) {
            progressContainer.style.display = 'none';
        }
    }

    // Error handling
    showUploadError(message) {
        window.app.ui.showToast(message, 'error');
        
        // Add error styling to preview if visible
        const previewCard = document.querySelector('.file-preview-card');
        if (previewCard) {
            previewCard.classList.add('upload-error');
        }
    }

    // Success handling
    showUploadSuccess(message) {
        window.app.ui.showToast(message, 'success');
        
        // Add success styling to preview if visible
        const previewCard = document.querySelector('.file-preview-card');
        if (previewCard) {
            previewCard.classList.add('upload-success');
        }
    }
}

// Initialize file upload manager
window.fileUploadManager = new FileUploadManager();