#!/usr/bin/env python3
"""Print `groupId:artifactId:version` for the reactor modules in scope, without running Maven.

Usage: list-gavs.py <root-dir> <modules> [<pom>]
  <modules>  "ALL", or a comma-separated -pl list: groupId:artifactId, :artifactId or a module path
  <pom>      the reactor pom, relative to <root-dir> (mvn -f); empty or omitted = pom.xml

Why not `mvn exec:exec`: it resolves each module's dependencies, so a module depending on another
NEW (not yet published, not yet built) GAV aborts the whole listing and every module after it
silently disappears from the check (nothing new is built or deployed).
Here the effective version is computed from the poms only (own <version> or the parent's,
${revision} and other ${property} placeholders resolved, -D properties of .mvn/maven.config included).
ALL = the modules of <modules> plus those of the profiles active by default (what a plain `mvn` builds);
an explicit -pl entry may also name a module of a non-default profile.
Not supported (exits non-zero rather than guess): properties defined only in a parent outside the reactor.
Profiles activated otherwise than by default (property, jdk, file) are not followed for ALL.
Exits non-zero, with the reason on stderr, if a module is unknown, ambiguous or missing on disk, or a
version cannot be resolved: a partial list would silently skip modules.
"""
import os
import re
import shlex
import sys
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"


def fail(msg):
    sys.exit("list-gavs: " + msg)


def child(el, name):
    return el.find(NS + name) if el is not None else None


def text(el, name):
    c = child(el, name)
    return c.text.strip() if c is not None and c.text else None


def module_list(el):
    return [m.text.strip() for m in el.findall(NS + "modules/" + NS + "module") if m.text and m.text.strip()]


def load(pom_file):
    try:
        root = ET.parse(pom_file).getroot()
    except (OSError, ET.ParseError) as e:
        fail("cannot read %s: %s" % (pom_file, e))
    parent = child(root, "parent")
    props = {}
    pe = child(root, "properties")
    if pe is not None:
        for p in pe:
            if isinstance(p.tag, str):
                props[p.tag.replace(NS, "")] = (p.text or "").strip()
    modules = [(m, True) for m in module_list(root)]
    for prof in root.findall(NS + "profiles/" + NS + "profile"):
        default = text(child(prof, "activation"), "activeByDefault") == "true"
        modules += [(m, default) for m in module_list(prof)]
    return {
        "file": pom_file,
        "artifactId": text(root, "artifactId"),
        "groupId": text(root, "groupId") or text(parent, "groupId"),
        "version": text(root, "version"),
        "parentGroupId": text(parent, "groupId"),
        "parentVersion": text(parent, "version"),
        "parent": (text(parent, "groupId"), text(parent, "artifactId")) if parent is not None else None,
        "props": props,
        "modules": modules,
    }


def pom_of(path):
    return os.path.join(path, "pom.xml") if os.path.isdir(path) else path


def walk(pom_file, in_all, acc, order):
    pom_file = os.path.normpath(pom_file)
    if pom_file in acc:
        acc[pom_file]["in_all"] |= in_all
        return
    mod = load(pom_file)
    mod["in_all"] = in_all
    acc[pom_file] = mod
    order.append(pom_file)
    for m, default in mod["modules"]:
        sub = pom_of(os.path.join(os.path.dirname(pom_file), m))
        if os.path.isfile(sub):
            walk(sub, in_all and default, acc, order)
        elif default:
            # Maven fails on a missing module: skipping it would silently drop it (and its submodules) from the check
            fail("module %s declared in %s does not exist" % (m, pom_file))


def cli_props(base):
    """-Dkey=value of .mvn/maven.config (e.g. -Drevision=...): they override the pom properties."""
    props = {}
    cfg = os.path.join(base, ".mvn", "maven.config")
    if os.path.isfile(cfg):
        with open(cfg) as f:
            for arg in shlex.split(f.read()):
                if arg.startswith("-D") and "=" in arg:
                    k, _, v = arg[2:].partition("=")
                    props[k] = v
    return props


def props_of(mod, by_ga, extra):
    """Properties visible from `mod`: its own over its ancestors' (reactor parents only), the project.* ones,
    then the -D ones (Maven user properties win over the model)."""
    chain, seen = [], set()
    m = mod
    while m and id(m) not in seen:
        seen.add(id(m))
        chain.append(m["props"])
        m = by_ga.get(m["parent"]) if m["parent"] else None
    merged = {}
    for p in reversed(chain):
        merged.update(p)
    merged.update({
        "project.groupId": mod["groupId"] or "",
        "project.artifactId": mod["artifactId"] or "",
        "project.version": mod["version"] or mod["parentVersion"] or "",
        "project.parent.groupId": mod["parentGroupId"] or "",
        "project.parent.version": mod["parentVersion"] or "",
    })
    merged.update(extra)
    return merged


def resolve(value, props, what):
    for _ in range(20):
        new = re.sub(r"\$\{([^}]+)\}", lambda m: props.get(m.group(1), m.group(0)), value)
        if new == value:
            break
        value = new
    if not value or "${" in value:
        fail("cannot resolve %s = %r" % (what, value))
    return value


def main():
    if len(sys.argv) < 3:
        fail("usage: list-gavs.py <root-dir> <modules> [<pom>]")
    base, scope = sys.argv[1], sys.argv[2]
    pom = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] else "pom.xml"
    root_pom = os.path.normpath(pom_of(os.path.join(base, pom)))
    acc, order = {}, []
    walk(root_pom, True, acc, order)
    mods = [acc[f] for f in order]
    by_ga = {(m["groupId"], m["artifactId"]): m for m in mods}
    by_path = {os.path.dirname(m["file"]): m for m in mods}
    by_path.update({m["file"]: m for m in mods})
    if scope == "ALL":
        wanted = [m for m in mods if m["in_all"]]
    else:
        wanted = []
        for sel in filter(None, (s.strip() for s in scope.split(","))):
            if ":" in sel:
                g, _, a = sel.partition(":")
                found = [m for m in mods if m["artifactId"] == a and (not g or m["groupId"] == g)]
            else:
                # module path, relative to the reactor root (the directory of <pom>)
                p = os.path.normpath(os.path.join(os.path.dirname(root_pom), sel))
                found = [by_path[p]] if p in by_path else []
            if len(found) != 1:
                fail("module %s %s" % (sel, "is ambiguous" if found else "is not part of the reactor"))
            if found[0] not in wanted:
                wanted.append(found[0])
    extra = cli_props(base)
    for mod in wanted:
        ga = (mod["groupId"], mod["artifactId"])
        version = resolve("${project.version}", props_of(mod, by_ga, extra), "version of %s:%s" % ga)
        print("%s:%s:%s" % (ga[0], ga[1], version))


if __name__ == "__main__":
    main()
