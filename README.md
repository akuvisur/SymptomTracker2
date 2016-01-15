# SymptomTracker2
Next iteration of the symptom tracker application for Master's Thesis

1) git clone this project

2) git clone the required AWARE plugin(s)</br>
    - Activity Recognition: https://github.com/denzilferreira/com.aware.plugin.google.activity_recognition.git</br>

3) Add a new module to this project:</br>
    - Select existing Android Studio Project</br>
    - browser to module directory (Activity Recognition)</br>
    - Go to module app/build.gradle</br>
        - change line: apply plugin: 'com.android.application' to: apply plugin: "com.android.library"</br>
        - comment out applicationId (libraries cannot have applicationId) ("//applicationId ...")</br>

4) Include "compile project(':com.aware.plugin.google.activity_recognition')" to main project app/build.gradle if not there already</br>

5) Build project</br>
    - Change plugin's buildToolsVersion if required</br>
