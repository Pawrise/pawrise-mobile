# Image de build Android : JDK 21 + SDK (cmdline-tools, platform 37, build-tools 36, défaut d'AGP 9.5).
# Sert au build reproductible en local (`docker compose run --rm android ./gradlew …`) et en CI.
FROM eclipse-temurin:21-jdk-noble

ARG CMDLINE_TOOLS_VERSION=16111833
ARG ANDROID_PLATFORM=android-37.0
ARG BUILD_TOOLS=36.0.0
ARG UID=1000
ARG GID=1000

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/home/builder/.gradle
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip git \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && curl -fsSL -o /tmp/cmdline-tools.zip \
       "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
    && mv /tmp/cmdline-tools/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" > /dev/null

# L'image de base embarque déjà un utilisateur 1000 (ubuntu) : on le renomme en builder.
RUN (getent passwd ${UID} && usermod -l builder -d /home/builder -m "$(getent passwd ${UID} | cut -d: -f1)") \
       || useradd -m -u ${UID} builder \
    && mkdir -p /home/builder/.gradle /workspace \
    && chown -R ${UID}:${GID} /home/builder "${ANDROID_HOME}" /workspace

USER builder
WORKDIR /workspace/android
