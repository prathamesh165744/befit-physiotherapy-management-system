import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';

export default function Register() {
  const [formData, setFormData] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await axios.post('http://localhost:8080/api/auth/register', formData);
      alert('Registration successful! Please log in.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data || 'Registration failed');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center pt-10 pb-20">
      <div className="w-full max-w-4xl px-4">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Patient Registration</h1>
            <p className="text-gray-500 mt-1">Please complete your details to onboard to the clinic system.</p>
          </div>
          <span className="bg-blue-100 text-blue-700 px-4 py-2 rounded-full text-sm font-medium">Step 1 of 1</span>
        </div>

        {/* Progress Tracker */}
        <div className="flex justify-between items-center mb-10 relative">
          <div className="absolute left-0 top-1/2 w-full h-0.5 bg-gray-200 -z-10"></div>
          <div className="flex flex-col items-center">
            <div className="w-10 h-10 bg-blue-600 text-white rounded-full flex items-center justify-center font-bold shadow-md">1</div>
            <span className="text-xs font-bold text-blue-600 mt-2">ACCOUNT DETAILS</span>
          </div>
          <div className="flex flex-col items-center opacity-40">
            <div className="w-10 h-10 bg-white border-2 border-gray-300 text-gray-400 rounded-full flex items-center justify-center font-bold">2</div>
            <span className="text-xs font-bold text-gray-500 mt-2">MEDICAL HISTORY</span>
          </div>
          <div className="flex flex-col items-center opacity-40">
            <div className="w-10 h-10 bg-white border-2 border-gray-300 text-gray-400 rounded-full flex items-center justify-center font-bold">3</div>
            <span className="text-xs font-bold text-gray-500 mt-2">EMERGENCY</span>
          </div>
        </div>

        {error && <div className="mb-6 p-4 bg-red-100 text-red-700 rounded-lg">{error}</div>}

        {/* Form Card */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden mb-12">
          <div className="bg-slate-50/50 p-6 border-b border-gray-100 flex items-center space-x-4">
            <div className="bg-blue-100 text-blue-600 p-3 rounded-lg">👤</div>
            <div>
              <h2 className="text-xl font-bold text-gray-900">Account Information</h2>
              <p className="text-sm text-gray-500">Collect basic identity and access details.</p>
            </div>
          </div>

          <form onSubmit={handleRegister} className="p-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  placeholder="Enter your full name"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="name@example.com"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                <input
                  type="text"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="e.g. 555-123-4567"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  required
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Create a strong password"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  required
                />
              </div>
            </div>

            <div className="flex justify-between items-center border-t border-gray-100 pt-6">
              <Link to="/login" className="text-gray-500 hover:text-gray-700 font-medium">Cancel</Link>
              <button
                type="submit"
                className="bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-8 rounded-lg transition-colors flex items-center"
              >
                Create Account &rarr;
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}