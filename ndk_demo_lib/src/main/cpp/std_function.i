%{
#include <functional>
%}

namespace std {
        template<typename> class function;
}

%template(Callback) std::function<void(const double&)>;