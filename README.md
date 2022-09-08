# DeeplyRecorder

## Deploy to Maven Central

1. Prepare the deployment. Especially version name and version code.

2. Upload artifact

```
./gradlew publishReleasePublicationToSonatypeRepository
```

3. 

Go to `https://s01.oss.sonatype.org/`. 
You can see your library in 'Staging Repositories'.

    1. Click 'Close' button - Confirm
    2. Wait until 'Release' button is activated by clicking 'Refresh' button  
    3. Click 'Release' button
