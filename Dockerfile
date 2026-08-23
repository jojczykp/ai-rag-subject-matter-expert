# syntax=docker/dockerfile:1.7

FROM node:25.2.1-bookworm-slim AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:26-jdk-noble AS backend-build
WORKDIR /workspace
COPY . ./
COPY --from=frontend-build /workspace/frontend/dist/ backend/src/main/resources/static/
RUN ./gradlew :backend:bootJar --no-daemon \
    && set -- backend/build/libs/*.jar \
    && test "$#" -eq 1 \
    && cp "$1" /workspace/app.jar

FROM ubuntu:24.04 AS llama-runtime
ARG TARGETARCH
ARG LLAMA_CPP_VERSION=b9892
ARG LLAMA_CPP_AMD64_SHA256=12084e76f775cfeae1d8109abde00f4a51637a9f1c0423f9fce25997340138d9
ARG LLAMA_CPP_ARM64_SHA256=e24c3c2c8f595819093f2f4e613e7a83a935e5d24ece7efc2317dc561fcee289
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && case "${TARGETARCH}" in \
        amd64) archive_arch=x64; archive_sha256="${LLAMA_CPP_AMD64_SHA256}" ;; \
        arm64) archive_arch=arm64; archive_sha256="${LLAMA_CPP_ARM64_SHA256}" ;; \
        *) echo "Unsupported target architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && archive="llama-${LLAMA_CPP_VERSION}-bin-ubuntu-${archive_arch}.tar.gz" \
    && curl --fail --location --retry 3 \
        "https://github.com/ggml-org/llama.cpp/releases/download/${LLAMA_CPP_VERSION}/${archive}" \
        --output "/tmp/${archive}" \
    && echo "${archive_sha256}  /tmp/${archive}" | sha256sum --check --strict \
    && mkdir -p /tmp/llama /opt/aisme/bin \
    && tar --extract --gzip --file "/tmp/${archive}" --directory /tmp/llama \
    && find -L /tmp/llama -type f \( -name 'llama-server' -o -name '*.so' -o -name '*.so.*' \) \
        -exec cp --dereference '{}' /opt/aisme/bin/ \; \
    && test -x /opt/aisme/bin/llama-server

FROM eclipse-temurin:26-jre-noble AS runtime
RUN apt-get update \
    && apt-get install --yes --no-install-recommends ca-certificates curl libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 aisme \
    && useradd --uid 10001 --gid aisme --no-create-home --home-dir /var/lib/aisme --shell /usr/sbin/nologin aisme \
    && mkdir -p /opt/aisme /var/lib/aisme/models \
    && chown -R aisme:aisme /opt/aisme /var/lib/aisme

COPY --from=llama-runtime --chown=aisme:aisme /opt/aisme/bin/ /opt/aisme/bin/
COPY --from=backend-build --chown=aisme:aisme /workspace/app.jar /opt/aisme/app.jar

ENV LD_LIBRARY_PATH=/opt/aisme/bin \
    SPRING_PROFILES_ACTIVE=container,minimal
WORKDIR /var/lib/aisme
USER aisme
EXPOSE 8080
VOLUME ["/var/lib/aisme/models"]
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/opt/aisme/app.jar"]
