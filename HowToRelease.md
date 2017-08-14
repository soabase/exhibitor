# How to Release Exhibitor


## Prerequisites

1. You must first be authorized. An existing Exhibitor committer must do this.
2. You should be comfortable using Apache Maven

## Maven Settings

Your Maven settings (~/.m2/settings.xml) file should have entries for the Sonatype servers:

```xml
<settings>
    ...
    <servers>
        <server>
            <id>sonatype-nexus-staging</id>
            <username>Your Sonatype OSS Username</username>
            <password>PASSWORD-ENCODED</password>
        </server>
 
        <server>
            <id>sonatype-nexus-snapshots</id>
            <username>Your Sonatype OSS Username</username>
            <password>PASSWORD-ENCODED</password>
        </server>
        ...
    </servers>
    ...
</settings>
```

Note: you can store encrypted passwords in your settings.xml if you want. Read the details here: http://maven.apache.org/guides/mini/guide-encryption.html

## Process

The release is done from your local machine. The process is similar to releasing an Apache open source project.

1. Ensure that you have created  a GPG key and uploaded it to a public GPG server. Maven will also try to run `gpg` so make sure its on your `PATH`. Also make sure your `master` branch is up to date.
2. The Maven commands to create the release (note there is considerable time between each):
  - `mvn -P oss release:prepare`: this will modify pom with new version and commit/push changes to origin
  - `mvn -P oss release:perform`: this uploads poms/jars to Sonatype maven repository
  - `git push --tags`: this will push release tags created by previos commands to github (may not be required)
3. Promote the release in the Sonatype Nexus repository:
  - Go to: https://oss.sonatype.org and login
  - Select Staging Repositories under the Build Promotion section on the left hand side
  - Select the repository from the main window, it should start with `iosoabase`
  - Select the Content tab at the bottom of the screen and navigate through the artifact tree and double check things
  - Close the Nexus staging repo by clicking on the soasbase-exhibitor repo and clicking the "Close" button
  - Release it by clicking the "Release" button (note: you need to wait a few minutes for "close" to complete
4. The release is now complete. Send an email to the Exhibitor user list, etc.

