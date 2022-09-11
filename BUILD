load("@rules_java//java:defs.bzl", "java_binary")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

java_binary(
    name = "RemoteCache",
    srcs = glob(["src/main/java/com/gette/*.java"]),
    main_class = "com.gette.RemoteCache",
    runtime_deps = [
        "@io_grpc_grpc_java//netty",
    ],
    deps = [
        ":remote_execution_grpc_java",
        ":remote_execution_proto_java",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//protobuf",
        "@io_grpc_grpc_java//stub",
        #This allows us to get list of implemented GRPC Services and Methods
        "@io_grpc_grpc_java//services:reflection",
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
