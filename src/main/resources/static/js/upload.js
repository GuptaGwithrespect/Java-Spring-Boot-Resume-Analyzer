const input = document.getElementById('resumeFile');
    const preview = document.getElementById('filePreview');
    const nameSpan = document.getElementById('fileName');
    const sizeSpan = document.getElementById('fileSize');

    input.addEventListener('change', function () {
      if (this.files && this.files.length > 0) {
        const file = this.files[0];
        nameSpan.textContent = file.name;
        sizeSpan.textContent = (file.size / 1024).toFixed(2) + ' KB';
        preview.classList.remove('hidden');
      }
    });
function validateForm() {
    const jobRole = document.getElementById("jobRole").value.trim();
    if (jobRole === "") {
      alert("Please select or enter a job role.");
      return false;
    }
    return true;
  }