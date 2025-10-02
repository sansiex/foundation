// Simple HTML sanitizer for markdown content
window.DOMPurify = {
    sanitize: function(html, options) {
        // Create a temporary div to parse HTML
        const temp = document.createElement('div');
        temp.innerHTML = html;
        
        // Define allowed tags and attributes
        const allowedTags = options?.ALLOWED_TAGS || [
            'p', 'br', 'strong', 'em', 'u', 'code', 'pre', 'blockquote', 
            'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'a',
            'table', 'thead', 'tbody', 'tr', 'th', 'td', 'div'
        ];
        
        const allowedAttrs = options?.ALLOWED_ATTR || [
            'href', 'target', 'rel', 'class', 'title', 'align'
        ];
        
        // Function to clean element
        function cleanElement(element) {
            // Check if tag is allowed
            if (!allowedTags.includes(element.tagName.toLowerCase())) {
                // Replace with text content
                const textNode = document.createTextNode(element.textContent || '');
                element.parentNode?.replaceChild(textNode, element);
                return;
            }
            
            // Clean attributes
            const attrs = Array.from(element.attributes);
            attrs.forEach(attr => {
                if (!allowedAttrs.includes(attr.name.toLowerCase())) {
                    element.removeAttribute(attr.name);
                }
            });
            
            // Special handling for links
            if (element.tagName.toLowerCase() === 'a') {
                const href = element.getAttribute('href');
                if (!href || !href.match(/^https?:\/\//)) {
                    element.setAttribute('href', '#');
                }
                element.setAttribute('target', '_blank');
                element.setAttribute('rel', 'noopener noreferrer');
            }
            
            // Recursively clean children
            const children = Array.from(element.children);
            children.forEach(child => cleanElement(child));
        }
        
        // Clean all elements
        const elements = Array.from(temp.children);
        elements.forEach(element => cleanElement(element));
        
        return temp.innerHTML;
    }
};