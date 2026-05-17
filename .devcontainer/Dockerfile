# Kotlin Jupyter examples for GitHub Codespaces (cf. github/codespaces-jupyter)
FROM azul-zulu:25-jdk

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    git \
    sudo \
    && rm -rf /var/lib/apt/lists/*

ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

RUN groupadd --gid "${USER_GID}" "${USERNAME}" \
    && useradd -s /bin/bash --uid "${USER_UID}" --gid "${USER_GID}" -m "${USERNAME}" \
    && echo "${USERNAME} ALL=(root) NOPASSWD:ALL" > "/etc/sudoers.d/${USERNAME}" \
    && chmod 0440 "/etc/sudoers.d/${USERNAME}"

COPY .devcontainer/requirements.txt /tmp/requirements.txt
COPY .devcontainer/warm-jupyter-cache.sh /tmp/warm-jupyter-cache.sh
COPY .devcontainer/warmup.ipynb /tmp/warmup.ipynb
COPY .devcontainer/start-notebook.sh /usr/local/bin/start-notebook.sh

RUN chmod +x /tmp/warm-jupyter-cache.sh /usr/local/bin/start-notebook.sh

USER ${USERNAME}
WORKDIR /home/${USERNAME}

ENV KOTLIN_JUPYTER_JAVA_HOME="${JAVA_HOME}"
ENV KOTLIN_JUPYTER_JAVA_OPTS="--enable-native-access=ALL-UNNAMED"

RUN python3 -m venv /home/${USERNAME}/venv \
    && /home/${USERNAME}/venv/bin/pip install --no-cache-dir -r /tmp/requirements.txt

ENV PATH="/home/${USERNAME}/venv/bin:${PATH}"

# Resolve example-notebook dependencies (rektdeal, dataframe, transitives) into ~/.m2/repository.
RUN /tmp/warm-jupyter-cache.sh

USER root
RUN rm -f /tmp/warm-jupyter-cache.sh /tmp/requirements.txt /tmp/warmup.ipynb

USER ${USERNAME}
