A simple Maven plugin which makes releasing a project containing DAR file a bit easier. 
It is aimed at projects where DAR artifacts are mostly static and only a few JARs are updated between releases, 
for other cases it is recommended to build a DAR file each time during a release.

This plugin allows updating certain artifacts (usually JARs) in a prebuilt DAR file. For example, lets imagine there
is a DAR with 3 JARs which must be updated in a new release.
I assume that the 3 JARs are already built and present in the target folder. The DAR is located in the project top
level folder.

Here is a sample plugin definition:

```
<plugin>
   <groupId>com.metasys</groupId>
   <artifactId>dar-update-maven-plugin</artifactId>
   <version>1.0-SNAPSHOT</version>

   <configuration>
        <dar>MyDar.dar</dar>
        <targetPath>${project.build.directory}/MyDar.dar</targetPath>
        <verbose>false</verbose>
        <overwrite>true</overwrite>
        <mappings>
            <mapping>
                <artifactName>myapp-api-complete.jardef.artifact</artifactName>
                <contentPath>${project.build.directory}/api-complete-final.jar</contentPath>
            </mapping>
            <mapping>
                <artifactName>myapp-api.jardef.artifact</artifactName>
                <contentPath>${project.build.directory}/api-final.jar</contentPath>
            </mapping>
            <mapping>
                <artifactName>myapp-utils.jardef.artifact</artifactName>
                <contentPath>${project.build.directory}/api-utils-final.jar</contentPath>
            </mapping>
        </mappings>
    </configuration>

    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>dar-update</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

When build is executed you should see something like:

```
[INFO] --- dar-update-maven-plugin:1.0-SNAPSHOT:dar-update (default) @ dar-update-maven-plugin-test ---
Artifact's 'myapp-api-complete.jardef.artifact' content is located at: content/75/-14020575/jarFile.jar
Updating /bin/content/75/-14020575/jarFile.jarmodified on June 13, 2020 11:41:50 PM CEST with api-complete-final.jar size (11262)
Artifact's 'myapp-api.jardef.artifact' content is located at: content/58/1029851258/jarFile.jar
Updating /bin/content/58/1029851258/jarFile.jarmodified on June 13, 2020 11:41:54 PM CEST with api-final.jar size (123233)
Artifact's 'myapp-utils.jardef.artifact' content is located at: content/58/1029851258/jarFile.jar
Updating /bin/content/58/1029851258/jarFile.jarmodified on June 13, 2020 11:44:52 PM CEST with api-utils-final.jar size (423232)
Moving updated DAR to /home/kbryd/NetBeansProjects/dar-update-maven-plugin-test/target/MyDar.dar
```

And the updated DAR will be saved to target/MyDar.dar

