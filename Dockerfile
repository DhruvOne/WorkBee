FROM eclipse-temurin:17-jdk AS build
ARG SDK_VER=34
ARG BT_VER=34.0.0
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin

RUN apt-get update && apt-get install -y unzip wget && rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip && \
    unzip -q /tmp/tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/tools.zip

RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager "platform-tools" "platforms;android-${SDK_VER}" "build-tools;${BT_VER}"

WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew assembleDebug --no-daemon

# ---------- Stage 2: APK serve ----------
FROM nginx:alpine
COPY --from=build /src/app/build/outputs/apk/debug/*.apk /usr/share/nginx/html/workbee.apk
RUN printf 'server {\n listen 80;\n location / {\n  root /usr/share/nginx/html;\n  autoindex on;\n }\n}\n' > /etc/nginx/conf.d/default.conf
EXPOSE 80
