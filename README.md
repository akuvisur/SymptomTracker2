# SymptomTracker2
Next iteration of the symptom tracker application for Master's Thesis

1) git clone this project

2) git clone the required AWARE plugin(s)</br>
    - Activity Recognition: https://github.com/denzilferreira/com.aware.plugin.google.activity_recognition.git</br>

3) Add a new module to this project:</br>
    - Open Module Settings, click top left '+' icon, select 'add new Module'</br>
    - Select existing Android Studio Project</br>
    - browser to module directory (Activity Recognition)</br>
    - Go to module app/build.gradle</br>
        - change line: apply plugin: 'com.android.application' to: apply plugin: "com.android.library"</br>
        - comment out applicationId (libraries cannot have applicationId) ("//applicationId ...")</br>

4) Include "compile project(':com.aware.plugin.google.activity_recognition')" to main project app/build.gradle if not there already</br>

5) Change com.aware.plugin.google.activity_recognition.Plugin class variable ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION to final (public final static ..)</br>
    - Since it is used inside a switch statement and needs to be declared final..</br>

6) Change the activity recognition dependencies (build.gradle)</br>
    - Change aware-core version to same as the app/build.gradle</br>
    - include "compile 'com.koushikdutta.ion:ion:2.1.6'" due to aware-core-3.9.8 or higher requirements</br>

7) Build project</br>
    - Change plugin's buildToolsVersion if required</br>
