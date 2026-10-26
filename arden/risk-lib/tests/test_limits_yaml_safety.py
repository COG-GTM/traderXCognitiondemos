"""Regression test for SNYK-CODE-PY-YAMLLOAD-0091: limits files must not construct Python objects."""
import pytest
import yaml

from risk_lib.limits import load_limits


def test_hostile_python_tags_are_rejected(tmp_path):
    sentinel = tmp_path / "pwned"
    hostile = tmp_path / "limits.yaml"
    hostile.write_text(
        "limits:\n"
        f"  - counterparty: !!python/object/apply:builtins.open [{str(sentinel)!r}, 'w']\n"
        "    limit: 1\n"
    )

    with pytest.raises(yaml.constructor.ConstructorError):
        load_limits(str(hostile))

    assert not sentinel.exists()


def test_plain_limits_still_load(tmp_path):
    path = tmp_path / "limits.yaml"
    path.write_text("limits:\n  - counterparty: ACME\n    limit: 42\n")
    assert load_limits(str(path)) == {"ACME": 42.0}
