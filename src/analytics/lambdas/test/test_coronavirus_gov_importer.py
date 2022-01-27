from . import check_code


def test_code_is_valid():
    assert check_code('importers/coronavirus_gov_importer.py')
