"""Regression test for SNYK-CODE-PY-YAMLLOAD-0091: limits YAML must not construct Python objects."""
import pytest
import yaml

from risk_lib.limits import load_limits


def test_python_object_tags_are_rejected(tmp_path):
    marker = tmp_path / "pwned"
    hostile = tmp_path / "limits.yaml"
    hostile.write_text(
        "limits:\n"
        f"  - counterparty: !!python/object/apply:builtins.open [\"{marker}\", \"w\"]\n"
        "    limit: 1\n"
    )

    with pytest.raises(yaml.constructor.ConstructorError):
        load_limits(str(hostile))

    assert not marker.exists()


def test_plain_limits_still_load(tmp_path):
    cfg = tmp_path / "limits.yaml"
    cfg.write_text("limits:\n  - counterparty: ACME\n    limit: 5000000\n")

    assert load_limits(str(cfg)) == {"ACME": 5_000_000.0}
