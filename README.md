# Remote Cache Service

This is a simple project that implements some of [remote-apis](https://github.com/bazelbuild/remote-apis/blob/main/build/bazel/remote/execution/v2/remote_execution.proto)
mainly related to Remote Cache.

## Requirements

* Linux Distribution
* OpenJDK 11
* [bazel 5.2.0](https://docs.bazel.build/versions/main/install.html)

**It isn't guaranteed that project is buildable/executable in Windows Environment**

## How to build/execute
Bazel provides us with 2 very useful commands: build and run. The first builds arbitrary target
or all the targets within the project if **wildcard** is being passed:
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

## Remote Cache Workflow
This is a very basic workflow that doesn't cover up all API calls that are available but it's
quite enough to get a common understanding. Moreover, except point 1 order of below mentioned requests
may vary due to async nature of GRPC.

1. If `--remote_cache=grpc//url:port` is passed along with build command bazel sends GetCapabilities
request to the server to get a list of supported options. It doesn't use reflection to collect
the full list of implemented calls but checks some general information:
 - remote cache support (CacheCapabilities)
 - remote execution support (ExecutionCapabilities)
 - lowest supported version (LowApiVersion)
 - highest supported version (HighApiVersion)
 - and some other parameters
2. If server responds with `GetCapabilities.CacheCapabilities` bazel client checks whether ActionResult  of currently executed target
 has already been cached or not using `ActionCache.GetActionResult`.
3. After that bazel client defines the outputs of currently executed target and sends `FindMissingBlobs` request to clarify 
whether they're already present in remote cache (Content Addressable Storage) or not. Server responds with the list
of blobs that aren't present.
4. Bazel builds a target and sends `ByteStream.Write` request (if blob's size is higher than max message size) 
or `ContentAddressableStorage.BatchUpdateBlobs` to upload produced outputs.
5. bazel client sends `ActionCache.UpdateActionResult` to upload `ActionResult` to Action Cache. It's basically
a metadata about executed target with a list of dependent blobs from `CAS`. So server has to ensure
that dependent blobs are already present in `CAS` otherwise when client will try to reuse entries from `AC`
he will get "Invalid action cache entry" error.

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
are still using JUnit 4 it is also possible to write unit tests using [Junit 5](https://github.com/grpc/grpc-java/tree/master/examples/src/test/java/io/grpc/examples).
There's a custom rule and some marcos that simplify configuration and work with transitive dependencies.
I decided to place it directly in this project for simplicity, especially because
it's marked as a sample so might change or be moved any time.

- In some cases it is useful to look at grpc logs to understand how bazel communicates with remote cache.
Luckily, bazel supports `--experimental_remote_grpc_log=PATH_TO_LOG` flag that produces such log. It
creates a log of serialized protobufs but it is possible to convert it to human readable format using 
[tools_remote](https://github.com/bazelbuild/tools_remote). 
Please, read [readme section](https://github.com/bazelbuild/tools_remote#printing-grpc-log-files)
that explains how to do it.

## Lessons learned
- It's no secret that bazel doesn't provide good documentation nor big community that
can help solving problems. There're very few topics on StackOverflow so you have
to gather pieces to the puzzle from all the sources you can get 
(github issues, stack overflow, google groups). And when we're talking about Remote Execution API
there's literally nothing but the comments inside of proto file. In some cases it's opaque.
For instance, I've spent several hours trying to understand why client doesn't write anything to
CAS and figured out I misunderstood how FindMissingBlobs works.

- IDE support is poor. No highlights nor autocomplete slows down development process
because you have to generate GRPC related code first and then go through it just
to understand the format and imports required to make implementations of services work.
I'd say it should be a focus to improve IDE support (bazel's plugin reviews for Idea is around 2.5, not great),
otherwise it will continue to be a nightmare for end users (developers) to work with bazel.

- It might be useful to support both Gradle and bazel within the same project. Having
Gradle partially solves the problem of poor IDE support but introduces overhead
that you have to maintain 2 buildsystems in parallel. Perhaps, it is possible
to introduce some Gradle plugin that is capable of generating WORKSPACE and BUILD
files since it's not a huge problem for Gradle to work with transitive dependencies
in comparison to bazel (should be improved after more projects start to use [bzlmod](https://bazel.build/build/bzlmod?hl=en)).
There're some projects that can simplify migration from Gradle to bazel (e.g. [Grazel](https://github.com/grab/Grazel))
but it isn't fully covering the use case. I am more talking about a tool that can
keep both buildsystems running in parallel.

- Working with GRPC in Java is too verbose. Unfortunately, I had no extensive experience with
kotlin to pick it right away and decided to stick to Java. It was my mistake. Examples in
the Internet show that working with GRPC in kotlin is way more convenient and simple. The
same applicable to go. I was researching already existing implementations [bazel-remote](https://github.com/buchgr/bazel-remote)
and [bazel-buildfarm](https://github.com/bazelbuild/bazel-buildfarm) to get a better understanding and it was way easier to understand
how bazel-remote works even though I have literally zero experience in go coding (and yes, bazel-remote
is simpler because buildfarm also has remote execution capabilities).