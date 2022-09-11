load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

git_repository(
    name = "google_remote_apis",
    remote = "https://github.com/bazelbuild/remote-apis.git",
    commit = "aa29b91f336b9be2c5370297210b67a6654c0b72",
)

# Needed for the googleapis protos.
# Version of googleapis is taken from remote-apis repository
# to ensure compatibility.
http_archive(
    name = "googleapis",
    #There's no need to explicitly set path to external/BUILD.googleapis
    #as bazel automatically assumes all BUILD files for external dependencies
    #described in WORKSPACE file are placed in external/ directory in
    #the root folder of the project
    build_file = "BUILD.googleapis",
    sha256 = "7b6ea252f0b8fb5cd722f45feb83e115b689909bbb6a393a873b6cbad4ceae1d",
    strip_prefix = "googleapis-143084a2624b6591ee1f9d23e7f5241856642f4d",
    urls = ["https://github.com/googleapis/googleapis/archive/143084a2624b6591ee1f9d23e7f5241856642f4d.zip"],
)

#GRPC Rules for Java
http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "89d16804d87a0d63878d71610b8c2245138f43de6a01ab5f7bad67d3a31e9f68",
    strip_prefix = "grpc-java-1.49.0",
    urls = ["https://github.com/grpc/grpc-java/archive/refs/tags/v1.49.0.zip"]
)

#Java GRPC Dependencies
http_archive(
    name = "rules_jvm_external",
    sha256 = "c21ce8b8c4ccac87c809c317def87644cdc3a9dd650c74f41698d761c95175f3",
    strip_prefix = "rules_jvm_external-1498ac6ccd3ea9cdb84afed65aa257c57abf3e0a",
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/1498ac6ccd3ea9cdb84afed65aa257c57abf3e0a.zip",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_ARTIFACTS")
load("@io_grpc_grpc_java//:repositories.bzl", "IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS")
load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

load("@com_google_protobuf//:protobuf_deps.bzl", "PROTOBUF_MAVEN_ARTIFACTS")

maven_install(
    artifacts = [
        "com.google.api.grpc:grpc-google-cloud-pubsub-v1:0.1.24",
        "com.google.api.grpc:proto-google-cloud-pubsub-v1:0.1.24",
    ] + IO_GRPC_GRPC_JAVA_ARTIFACTS + PROTOBUF_MAVEN_ARTIFACTS,
    generate_compat_repositories = True,
    override_targets = IO_GRPC_GRPC_JAVA_OVERRIDE_TARGETS,
    repositories = [
        "https://repo.maven.apache.org/maven2/",
    ],
)

load("@maven//:compat.bzl", "compat_repositories")

compat_repositories()

#Dependencies required by junit5
load("//bazel/junit5:junit5.bzl", "junit_jupiter_java_repositories", "junit_platform_java_repositories")

JUNIT_JUPITER_VERSION = "5.9.0"

JUNIT_PLATFORM_VERSION = "1.9.0"

junit_jupiter_java_repositories(
    version = JUNIT_JUPITER_VERSION,
)

junit_platform_java_repositories(
    version = JUNIT_PLATFORM_VERSION,
)
