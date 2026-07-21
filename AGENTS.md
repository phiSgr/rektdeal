# Agent notes for ReKtDeal

## Public API surface

Before writing deal-generation or simulation code, **read [`interface.txt`](interface.txt)**.

It is a condensed dump of the published Kotlin API (dds4j + rektdeal): types, constructors, and function signatures without bodies. Prefer it over guessing from memory or scraping Maven sources.

Also skim human-authored examples under [`examples/`](examples/) (`introduction.ipynb`, `gambling.ipynb`, `opening_lead.ipynb`, `gazzilli_weak_response.ipynb`). Notebooks under [`examples/vibed/`](examples/vibed/) were produced by an agent and are not canonical tutorials.

## Reading example notebooks

Prefer converting to a script for reading, not the raw `.ipynb` JSON:

```sh
cd examples && source venv/bin/activate
jupyter nbconvert --to script --stdout introduction.ipynb
```

Or extract code cells with a short Python/`jq` one-liner. The JSON form is noisy (outputs, metadata, escaped newlines) and burns context; use it only when you must preserve cell structure for editing.

## Editing notebooks

**Do not edit a notebook while it is open in the VS Code / Cursor notebook UI.** The editor may rewrite the file and overwrite agent edits. Close the tab, or edit a copy, then reopen.

`EditNotebook` is fine when the file is closed. For bulk rewrites, rewriting the `.ipynb` JSON (or regenerating via a script) is more reliable than many small cell edits.

## Executing notebooks / the `'name' is a required property` error

Prefer `jupyter execute` over `jupyter nbconvert --execute` / `--clear-output`. The latter validate against `nbformat` first and can fail with:

```
nbformat.validator.NotebookValidationError: 'name' is a required property
On instance['cells'][N]['outputs'][0]:
```

```sh
cd examples && source venv/bin/activate
export JAVA_HOME=$(/usr/libexec/java_home -v 22) # or higher
jupyter execute --inplace vibed/opening_structure_probs.ipynb
```

## Jupyter runtime

See [`examples/README.md`](examples/README.md). Use a published Maven Central version in `@file:DependsOn(...)` unless the task explicitly needs a local build (`@file:Repository("*mavenLocal")`).
