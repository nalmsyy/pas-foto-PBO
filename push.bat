git add .
set /p pesan="Pesan commit: "
git commit -m "%pesan%"
git push -u origin main