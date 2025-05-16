# SystemService - Android Background Monitoring Service

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

> **Avertizare legală**: Acest proiect este destinat exclusiv în scop educațional și de cercetare în domeniul securității mobile. Utilizarea lui pe dispozitive fără consimțământ expres este ilegală în majoritatea jurisdicțiilor.

## 📌 Descriere

Acest serviciu Android rulează în fundal și monitorizează:
- 📞 Apelurile primite
- 📩 Mesajele SMS primite
- 🔔 Notificări (inclusiv WhatsApp/Messenger)

Datele sunt transmise către Firebase Firestore pentru analiză.

## 🛠 Tehnologii utilizate

- Kotlin 1.8+
- Android SDK 21+
- Firebase Firestore
- Foreground Services
- Broadcast Receivers

## 🔧 Configurare

### Cerințe preliminare
- Android Studio Flamingo sau mai nou
- Dispozitiv Android sau emulator cu API 21+
- Cont Firebase

### Instalare
1. Clonează repository-ul:
   ```bash
   git clone https://github.com/username/SystemService.git