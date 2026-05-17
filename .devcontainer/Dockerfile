# Codespace for Kotlin Jupyter examples (cf. github/codespaces-jupyter)
FROM mcr.microsoft.com/devcontainers/universal:5

ARG USERNAME=codespace

USER root

ENV KOTLIN_JUPYTER_JAVA_OPTS=--enable-native-access=ALL-UNNAMED

COPY .devcontainer/requirements.txt /tmp/requirements.txt
COPY .devcontainer/warm-jupyter-cache.sh /tmp/warm-jupyter-cache.sh
COPY .devcontainer/warmup.ipynb /tmp/warmup.ipynb
RUN chmod +x /tmp/warm-jupyter-cache.sh

USER ${USERNAME}
WORKDIR /home/${USERNAME}

ENV PATH="/home/${USERNAME}/.local/bin:${PATH}"

# Install as codespace (not root) so Jupyter/VS Code see the same kernelspecs.
RUN pip3 install --no-cache-dir -r /tmp/requirements.txt \
    && /tmp/warm-jupyter-cache.sh

USER root
RUN rm -f /tmp/warm-jupyter-cache.sh /tmp/requirements.txt /tmp/warmup.ipynb

USER ${USERNAME}
