// Логика переключения между разделами
document.querySelectorAll(".menu-button").forEach((button) => {
    button.addEventListener("click", () => {
        // Скрываем все секции
        document.querySelectorAll(".converter-section").forEach((section) => {
            section.classList.add("hidden");
        });

        // Показываем выбранную секцию
        const sectionId = button.getAttribute("data-section");
        document.getElementById(sectionId).classList.remove("hidden");

        // Обновляем активное состояние кнопок
        document.querySelectorAll(".menu-button").forEach((btn) => btn.classList.remove("active"));
        button.classList.add("active");
    });
});
