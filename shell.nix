{pkgs ? import <nixpkgs> {
  config = {
    packageOverrides = pkgs: {
      sbt = pkgs.sbt.override { jre = pkgs.openjdk17; };
    };
  };
}} :
pkgs.mkShell {
  # sbt-protoc downloads a Linux protoc binary that needs the C++ runtime.
  LD_LIBRARY_PATH = pkgs.lib.optionalString pkgs.stdenv.isLinux
    (pkgs.lib.makeLibraryPath [ pkgs.stdenv.cc.cc.lib ]);

  buildInputs = [
    pkgs.sbt
    pkgs.openjdk17
    pkgs.nodejs

    # keep this line if you use bash
    pkgs.bashInteractive
  ];
}
