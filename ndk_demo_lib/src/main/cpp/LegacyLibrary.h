//
// Created by eriche on 2024/8/24.
//

#pragma once

#include <string>

namespace LegacyLibrary {
    class LegacyClass {
    public:
        const std::string& get_property() { return property; }
        void set_property(const std::string& property) { this->property = property; }
        std::string property;
    };
}
