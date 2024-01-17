# face-data-matching-app

FaceDataMatchingApp is an Android application developed in Kotlin, designed for facial recognition and data management. With this app, users can capture and store facial data, particularly focused on medical information (although adaptable to various use cases). The app provides functionality to verify if a scanned face is already registered in the database. If a match is found, the associated medical data is displayed.

Features:
* Face Registration: Scan and record facial data for users.
* Face Verification: Check if a scanned face is present in the database.
* Dynamic Data Display: Display relevant medical data upon successful face recognition.
Face Recognition Process:
The face recognition process utilizes a face matching model, comparing two faces based on their output vectors. The degree of similarity between these vectors is evaluated against a user-defined threshold. If the similarity surpasses the threshold, the app considers the faces matched, triggering the display of associated medical data.

Feel free to customize the threshold value to suit your preferences and application requirements.
