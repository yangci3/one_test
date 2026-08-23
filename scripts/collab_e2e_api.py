# -*- coding: utf-8 -*-
"""Collab M1 API acceptance smoke test (no DB writes except optional merge)."""
import json
import subprocess
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:8080"


def captcha_login(username, password="admin123"):
    with urllib.request.urlopen(BASE + "/captchaImage") as r:
        cap = json.loads(r.read().decode("utf-8"))
    uuid = cap["uuid"]
    subprocess.run(
        ["redis-cli", "SET", f"captcha_codes:{uuid}", '"ab12"'],
        check=True,
        capture_output=True,
    )
    body = json.dumps(
        {"username": username, "password": password, "code": "ab12", "uuid": uuid}
    ).encode()
    req = urllib.request.Request(
        BASE + "/login",
        data=body,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req) as r:
        data = json.loads(r.read().decode("utf-8"))
    if data.get("code") != 200:
        raise RuntimeError(f"login failed for {username}: {data}")
    return data["token"]


def api(token, method, path, payload=None):
    headers = {"Authorization": "Bearer " + token}
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(body)
        except json.JSONDecodeError:
            return e.code, {"raw": body}


def assert_ok(label, code, body):
    if code != 200 or body.get("code") != 200:
        raise AssertionError(f"{label}: HTTP {code} body={body}")


def main():
    failures = []

    def check(name, fn):
        try:
            fn()
            print(f"PASS {name}")
        except Exception as exc:
            failures.append((name, exc))
            print(f"FAIL {name}: {exc}")

    admin = captcha_login("collab_admin")
    member = captcha_login("collab1")
    plain = captcha_login("plain_user")

    def admin_lists():
        for path in ["/collab/task/list", "/collab/doc/list", "/collab/user/candidates"]:
            c, b = api(admin, "GET", path)
            assert_ok(path, c, b)

    def admin_task_detail():
        c, b = api(admin, "GET", "/collab/task/1")
        assert_ok("task detail", c, b)
        assignments = b["data"]["assignments"]
        if not assignments:
            raise AssertionError("expected assignments on task 1")
        snap = assignments[0].get("contentSnapshot")
        if not snap:
            raise AssertionError("expected contentSnapshot on submitted assignment")

    def member_mine():
        c, b = api(member, "GET", "/collab/task/mine")
        assert_ok("mine list", c, b)
        if not b["data"]:
            raise AssertionError("collab1 should have assigned tasks")

    def plain_denied():
        c, b = api(plain, "GET", "/collab/task/list")
        if c == 200 and b.get("code") == 200:
            raise AssertionError("plain_user should not access collab task list")
        c, b = api(plain, "GET", "/collab/doc/list")
        if c == 200 and b.get("code") == 200:
            raise AssertionError("plain_user should not access collab doc list")

    def member_denied_admin_apis():
        for path in ["/collab/task/list", "/collab/doc/list", "/collab/user/candidates"]:
            c, b = api(member, "GET", path)
            if c == 200 and b.get("code") == 200:
                raise AssertionError(f"collab1 should not access {path}")

    def candidates_are_members():
        c, b = api(admin, "GET", "/collab/user/candidates")
        assert_ok("candidates", c, b)
        rows = b.get("data") or []
        if not rows:
            raise AssertionError("expected at least one collab member candidate")
        for row in rows:
            name = row.get("userName") or row.get("username")
            if name == "plain_user":
                raise AssertionError("plain_user must not appear in candidates")

    def merge_and_verify():
        c, b = api(admin, "GET", "/collab/task/1")
        assert_ok("task before merge", c, b)
        doc_id = b["data"]["task"]["docId"]
        c, b = api(admin, "POST", f"/collab/task/1/merge")
        assert_ok("merge", c, b)
        c, b = api(admin, "GET", f"/collab/doc/{doc_id}")
        assert_ok("doc after merge", c, b)
        html = b["data"].get("contentHtml") or ""
        if "sec-1" not in html:
            raise AssertionError("merged doc missing section sec-1")
        if "[unsubmitted:" in html:
            raise AssertionError("merge left unsubmitted placeholder in doc")

    check("admin lists", admin_lists)
    check("admin task detail + snapshot", admin_task_detail)
    check("member mine", member_mine)
    check("plain user denied", plain_denied)
    check("member denied admin APIs", member_denied_admin_apis)
    check("candidates exclude non-members", candidates_are_members)
    check("merge updates doc", merge_and_verify)

    if failures:
        print(f"\n{len(failures)} check(s) failed")
        sys.exit(1)
    print("\nAll collab E2E API checks passed")


if __name__ == "__main__":
    main()
