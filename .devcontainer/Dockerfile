# Codespace for Kotlin Jupyter examples (cf. github/codespaces-jupyter)
FROM azul-zulu:25-jdk AS zulu25

FROM mcr.microsoft.com/devcontainers/universal:5

ARG KOTLIN_LSP_VERSION=262.4739.0
ARG USERNAME=codespace

USER root

# Zulu 25 from the official image (kotlin-lsp requires JDK 25).
COPY --from=zulu25 /usr/lib/jvm/zulu25 /usr/lib/jvm/zulu25
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/zulu25/bin/java 2500 \
    && update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/zulu25/bin/javac 2500

ENV JAVA_HOME=/usr/lib/jvm/zulu25
ENV KOTLIN_JUPYTER_JAVA_HOME=/usr/lib/jvm/zulu25
ENV KOTLIN_JUPYTER_JAVA_OPTS=--enable-native-access=ALL-UNNAMED
# universal ships JDK 21 on PATH; kotlin-lsp requires 25.
ENV PATH="/usr/lib/jvm/zulu25/bin:${PATH}"

COPY .devcontainer/requirements.txt /tmp/requirements.txt
COPY .devcontainer/warm-jupyter-cache.sh /tmp/warm-jupyter-cache.sh
COPY .devcontainer/warmup.ipynb /tmp/warmup.ipynb
RUN chmod +x /tmp/warm-jupyter-cache.sh

USER ${USERNAME}
WORKDIR /home/${USERNAME}

RUN python3 -m venv /home/${USERNAME}/venv \
    && /home/${USERNAME}/venv/bin/pip install --no-cache-dir -r /tmp/requirements.txt

ENV PATH="/home/${USERNAME}/venv/bin:${PATH}"

RUN /tmp/warm-jupyter-cache.sh

USER root
RUN rm -f /tmp/warm-jupyter-cache.sh /tmp/requirements.txt /tmp/warmup.ipynb

USER ${USERNAME}
