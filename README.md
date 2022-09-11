# Remote Cache Service

This is a simple project that implements some of [remote-apis](https://github.com/bazelbuild/remote-apis/blob/main/build/bazel/remote/execution/v2/remote_execution.proto)
mainly related to Remote Cache.

## Requirements

* Linux Distribution
* OpenJDK 11
* [bazel 5.2.0](https://docs.bazel.build/versions/main/install.html)

===It isn't guaranteed that project is buildable/executable in Windows Environment===

## How to build/execute
Bazel provides us with 2 very useful commands: build and run. The first builds arbitrary target
or all the targets within the project if ==wildcard== is being passed:
```
# Wildcard to build all targets
bazel build //...

# Single target to build Server binary
bazel build :remote_cache_server_bin
```
It is also possible to run the binary from bazel if main_class and runtime_deps are defined
for a target with kind `java_binary`
```
bazel run :remote_cache_server_bin
```

This project also has some rudimentary unit tests that are usually executed along with build command.
But it is also possible to run them explicitly:
```
bazel test //...
```

### Standalone
TBD

## Hints
- If you're consuming any bazel project as external dependency (placed in WORKSPACE file)
you can consume targets from it. But don't forget to place transitive
dependencies of that external project into your WORKSPACE. For instance,
I am consuming [remote-apis](https://github.com/bazelbuild/remote-apis/) project that
depends on [googleapis](https://github.com/googleapis/googleapis). So I had to explicitly
add it (copy from WORKSPACE of remote-apis) as my dependency even though I am not 
referring it myself anywhere.

- You can write a minimal BUILD file and place in `external/` folder in the root of the project to build only those targets 
of external projects that you really need. For instance, remote-apis depends on a limited set of targets within googleapis
so there's no need to build it completely as it introduces a lot of other transitive dependencies that are pretty hard 
to track.

- It is possible to add any maven dependency you need within bazel project. You just need to use `maven_install` within
WORKSPACE file and place there coordinates of a binary you need. For instance:
```
maven_install(
    artifacts = ["commons-codec:commons-codec:1.15"],
    repositories = [
        "https://repo.maven.apache.org/maven2/"
    ],
)
```
This macro will generate maven targets and then they can be consumed as dependencies. 
```
java_binary(
    name = "some_target",
    srcs = glob([src/main/java/com/example/*.java]),
    deps = ["@maven//:commons_codec_commons_codec"],
)
```
To get a name of generated target you can use bazel query command that goes
over all build tree. The scope can be limited to maven only:
```
bazel query @maven//:all --output=build
```

- Bazel has native rules to build and test Java Code. Although a lot of examples (including official, 
e.g. [here](https://github.com/grpc/grpc-java/tree/master/examples/src/test/java/io/grpc/examples))
are still using JUnit 4 it is also possible to write unit tests using [Junit 5]
(https://github.com/grpc/grpc-java/tree/master/examples/src/test/java/io/grpc/examples).
There's a custom rule and some marcos that simplify configuration and work with transitive dependencies.
I decided to download it and place directly in this project for simplicity. Especially because
it's marked as a sample.