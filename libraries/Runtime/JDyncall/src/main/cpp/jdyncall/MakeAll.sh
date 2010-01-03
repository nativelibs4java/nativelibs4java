#!/bin/bash

./Make.sh $@ || exit 1

BUILD_CONFIG=debug ./Make.sh $@ || exit 1

