# face-data-matching-app

app demo: https://drive.google.com/file/d/1tzzSyaop94dVgg-fgXbjm2wP3VNo18CI/view?usp=sharing

FaceDataMatchingApp is an Android application developed in Kotlin, designed for facial recognition and data management. With this app, users can capture and store facial data, particularly focused on medical information (although adaptable to various use cases). The app provides functionality to verify if a scanned face is already registered in the database. If a match is found, the associated medical data is displayed.

Features:
1. Face Registration: Scan and record a face for users.
2. Face Verification: Check if a scanned face is present in the database.
3. Dynamic Data Display: Display relevant medical data upon successful face recognition.

Face Recognition Process:
The face recognition process utilizes a face matching model, comparing two faces based on their output vectors. The degree of similarity between these vectors is evaluated against a user-defined threshold. If the similarity surpasses the threshold, the app considers the faces matched, triggering the display of associated medical data. Feel free to customize the threshold value to suit your preferences and application requirements.

This is a high level summary of the app workflow. From the homepage, you first select "Register face" to register face. When a face is detected, the bounding box will appear around the user's face and then you will be able to click the register face button. If there is no face detected and you click the button, you will be prompted (watch the app demo video, link above). After clicking the button, you will be prompted to the next page when you can input your medical data.
![alt text](https://github.com/haidiazaman/face-data-matching-app/blob/main/imgs/photo_1_2024-01-26_00-04-53.jpg)

For the next step, in the homepage you click "Cross check medical records" to find a face match with an existing face match in the database (app uses SharedPreferences). If there is a face match, you will be advanced to the next page where your medical data will be displayed. Else, you will be prompted "No Face Match" if the app can't find a match with any existing face in the database or "No Face detected" if there is no face in camera view.
![alt text](https://github.com/haidiazaman/face-data-matching-app/blob/main/imgs/photo_2_2024-01-26_00-04-53.jpg)
