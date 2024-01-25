# face-data-matching-app

product demo: https://drive.google.com/file/d/1tzzSyaop94dVgg-fgXbjm2wP3VNo18CI/view?usp=sharing

FaceDataMatchingApp is an Android application developed in Kotlin, designed for facial recognition and data management. With this app, users can capture and store facial data, particularly focused on medical information (although adaptable to various use cases). The app provides functionality to verify if a scanned face is already registered in the database. If a match is found, the associated medical data is displayed.

Features:
1. Face Registration: Scan and record a face for users.
2. Face Verification: Check if a scanned face is present in the database.
3. Dynamic Data Display: Display relevant medical data upon successful face recognition.

Face Recognition Process:
The face recognition process utilizes a face matching model, comparing two faces based on their output vectors. The degree of similarity between these vectors is evaluated against a user-defined threshold. If the similarity surpasses the threshold, the app considers the faces matched, triggering the display of associated medical data.

Feel free to customize the threshold value to suit your preferences and application requirements.

![alt text](https://github.com/haidiazaman/face-data-matching-app/blob/main/imgs/face%20matched.png)
