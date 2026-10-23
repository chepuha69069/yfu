Apple Farm - автономный Fabric-мод (Minecraft 1.21.4), без Releon Client

СБОРКА ЧЕРЕЗ GITHUB (ничего ставить не нужно):
1) Создайте новый репозиторий на github.com (можно приватный)
2) Загрузите в него ВСЕ файлы этой папки (Add file -> Upload files),
   включая скрытую папку .github
3) Вкладка Actions -> Build Apple Farm -> дождитесь зелёной галочки (1-3 мин)
   (если не запустилось само: Run workflow)
4) Откройте завершённый запуск -> внизу Artifacts -> applefarm -> скачайте zip
5) Внутри applefarm-1.0.0.jar -> положите в mods (нужен Fabric API)
6) В игре в чате: .apple

Если папка .github не загрузилась: Add file -> Create new file, имя
.github/workflows/build.yml и вставьте содержимое из этого архива.

Локальная сборка на Windows: build.bat (нужен JDK 21).
