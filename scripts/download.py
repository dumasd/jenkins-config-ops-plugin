import requests
import os
from urllib.parse import urlparse


def download_file_from_unpkg(pkg: str, ver: str, files: list, dest_dir: str):
    for file in files:
        url = f"https://unpkg.com/{pkg}@{ver}/{file}"
        response = requests.get(url)
        dest_path = os.path.join(dest_dir, pkg, ver, file)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        if response.status_code == 200:
            with open(dest_path, "wb") as f:
                f.write(response.content)
                f.flush()
            print(f"Downloaded {file} from {url}")
        else:
            print(f"Failed to download {file}, from {url}, response: {response}")


if __name__ == "__main__":
    pkg_list = [
        {
            "pkg": "codemirror",
            "ver": "5.65.19",
            "files": [
                # "lib/codemirror.js",
                # "addon/merge/merge.js",
                # "mode/yaml/yaml.js",
                # "mode/properties/properties.js",
                # "mode/javascript/javascript.js",
                # "mode/xml/xml.js",
                # "addon/lint/lint.js",
                # "addon/lint/json-lint.js",
                # "addon/lint/yaml-lint.js",
                # "lib/codemirror.css",
                # "addon/merge/merge.css",
                # "addon/lint/lint.css",
            ],
        },
        {"pkg": "diff-match-patch", "ver": "1.0.5", "files": ["index.js"]},
        {"pkg": "js-yaml", "ver": "4.1.0", "files": ["dist/js-yaml.min.js"]},
    ]
    dest_dir = "src/main/webapp"
    for pkg in pkg_list:
        pkg_name = pkg["pkg"]
        pkg_ver = pkg["ver"]
        pkg_files = pkg["files"]
        download_file_from_unpkg(pkg_name, pkg_ver, pkg_files, dest_dir)
