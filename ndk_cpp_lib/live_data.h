//
// Created by eriche on 2024/8/1.
//

#ifndef KTDEMOCOLLECTION_LIVE_DATA_H
#define KTDEMOCOLLECTION_LIVE_DATA_H

template<typename T>
class LiveData
{
	T value;

public:
	virtual T getValue();

	void observerForever();

	void removeObserver();

};


#endif //KTDEMOCOLLECTION_LIVE_DATA_H
