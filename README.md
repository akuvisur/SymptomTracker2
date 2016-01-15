# SymptomTracker2
Next iteration of the symptom tracker application for Master's Thesis

1) git clone this project

2) git clone the required AWARE plugin(s)
    - Activity Recognition: https://github.com/denzilferreira/com.aware.plugin.google.activity_recognition.git

3) Add a new module to this project:
    - Select existing Android Studio Project
    - browser to module directory (Activity Recognition)
    - Go to module app/build.gradle
        - change line: apply plugin: 'com.android.application' to: apply plugin: "com.android.library"
        - comment out applicationId (libraries cannot have applicationId) ("//applicationId ...")

4) Include "compile project(':com.aware.plugin.google.activity_recognition')" to main project app/build.gradle if not there already

5) Build project
    - Change plugin's buildToolsVersion if required
