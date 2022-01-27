from . import check_code


def test_code_is_valid():
    assert check_code('importers/ios_android_ratings_importer.py')
