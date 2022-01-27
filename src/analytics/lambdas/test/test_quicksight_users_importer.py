from . import check_code


def test_code_is_valid():
    assert check_code('importers/quicksight_users_importer.py')
