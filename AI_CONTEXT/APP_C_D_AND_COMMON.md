# common-core and app-c/app-d entry points

This file is a lossless textual snapshot. Every section contains the complete current content of the source file named in its heading.

## File: common-core/src/main/java/com/example/commoncore/CommonCoreMarker.java

````java
package com.example.commoncore;

/** Marks the shared domain/core module. */
public final class CommonCoreMarker {

    private CommonCoreMarker() {
    }
}
````


## File: app-c/src/main/java/com/example/appc/AppCApplication.java

````java
package com.example.appc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppCApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppCApplication.class, args);
    }
}
````


## File: app-d/src/main/java/com/example/appd/AppDApplication.java

````java
package com.example.appd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppDApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppDApplication.class, args);
    }
}
````

