load("@rules_java//java:defs.bzl", "java_binary")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

java_binary(
    name = "remote_cache_server_bin",
    srcs = glob(["src/main/java/com/gette/*.java"]),
    main_class = "com.gette.RemoteCache",
    runtime_deps = [
        "@io_grpc_grpc_java//netty",
    ],
    deps = [
        ":google_rpc_status_java_proto",
        ":remote_execution_grpc_java",
        ":remote_execution_proto_java",
        ":remote_execution_semver_proto_java",
        #bazel offers to use @com_google_protobuf//java/core:lite
        #but it isn't possible due to package visibility.
        #Core package isn't publicly visible, hence,
        #can't be used in external projects.
        #Comment offers to use java/lite.
        "@com_google_protobuf//java/lite",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//protobuf",
        "@io_grpc_grpc_java//stub",
        "@maven//:commons_codec_commons_codec",
        #This allows us to get list of implemented GRPC Services and Methods
        "@io_grpc_grpc_java//services:reflection",
    ],
)

load("//bazel/junit5:junit5.bzl", "java_junit5_test")

java_junit5_test(
    name = "remote_cache_server_test",
    srcs = glob(["src/test/java/com/gette/*.java"]),
    test_package = "com.gette",
    deps = [
        ":remote_cache_server_bin",
        ":remote_execution_grpc_java",
        ":remote_execution_proto_java",
        ":remote_execution_semver_proto_java",
        "@io_grpc_grpc_java//core:inprocess",
        "@io_grpc_grpc_java//testing",
        "@maven//:commons_codec_commons_codec",
    ],
)

java_proto_library(
    name = "remote_execution_proto_java",
    deps = ["@google_remote_apis//build/bazel/remote/execution/v2:remote_execution_proto"],
)

java_grpc_library(
    name = "remote_execution_grpc_java",
    srcs = ["@google_remote_apis//build/bazel/remote/execution/v2:remote_execution_proto"],
    deps = [":remote_execution_proto_java"],
)

java_proto_library(
    name = "remote_execution_semver_proto_java",
    deps = ["@google_remote_apis//build/bazel/semver:semver_proto"],
)

#Although bazel offers to add
#@googleapis//:google_rpc_status_proto to //:remote_cache_server_bin directly
#This won't work. We need to compile java proto library first
#and pass it as a dependency to java_binary
java_proto_library(
    name = "google_rpc_status_java_proto",
    deps = ["@googleapis//:google_rpc_status_proto"],
)